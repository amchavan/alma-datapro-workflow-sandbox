from setuptools import setup

dependencies = ['adaptmb', 'adaptmb-demos-common']

setup(name='adaptmb-demos-sender',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/examples'],
      install_requires=dependencies,
      zip_safe=False)
