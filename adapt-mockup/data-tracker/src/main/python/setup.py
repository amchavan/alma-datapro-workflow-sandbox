from setuptools import setup

dependencies = [
    "adapt-mock-icd",
    "adaptmb"
]

setup(name='adapt-mock-data-tracker',
      version='0.1',
      description='DRAWS Mock DataTracker',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/archive'],
      install_requires=dependencies,
      zip_safe=False)
